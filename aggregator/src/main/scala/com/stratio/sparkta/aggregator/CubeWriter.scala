/**
 * Copyright (C) 2016 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.sparkta.aggregator

import java.sql.{Date, Timestamp}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparkta.sdk._
import org.apache.spark.sql._
import org.apache.spark.streaming.dstream.DStream

import scala.util.{Failure, Success, Try}

case class WriterOptions(outputs: Seq[String],
                         dateType: TypeOp.Value = TypeOp.Timestamp,
                         fixedMeasures: MeasuresValues = MeasuresValues(Map.empty),
                         isAutoCalculatedId: Boolean = false)

case class CubeWriter(cube: Cube,
                      tableSchema: TableSchema,
                      options: WriterOptions,
                      outputs: Seq[Output],
                      triggerOutputs: Seq[Output],
                      triggerSchemas: Seq[TableSchema])
  extends TriggerWriter with SLF4JLogging {

  val upsertOptions = tableSchema.timeDimension.fold(Map.empty[String, String]) { timeName =>
    Map(Output.TimeDimensionKey -> timeName)
  } ++ Map(Output.TableNameKey -> tableSchema.tableName,
    Output.IdAutoCalculatedKey -> tableSchema.isAutoCalculatedId.toString)

  def write(stream: DStream[(DimensionValuesTime, MeasuresValues)]): Unit = {
    stream.map { case (dimensionValuesTime, measuresValues) =>
      toRow(dimensionValuesTime, measuresValues)
    }.foreachRDD(rdd => {
      if (options.outputs.nonEmpty && rdd.take(1).length > 0) {
        val sqlContext = SQLContext.getOrCreate(rdd.context)
        val cubeDataFrame = sqlContext.createDataFrame(rdd, tableSchema.schema)

        options.outputs.foreach(outputName =>
          outputs.find(output => output.name == outputName) match {
            case Some(outputWriter) => Try(outputWriter.upsert(cubeDataFrame, upsertOptions)) match {
              case Success(_) =>
                log.debug(s"Data stored in ${tableSchema.tableName}")
              case Failure(e) =>
                log.error(s"Something goes wrong. Table: ${tableSchema.tableName}")
                log.error(s"Schema. ${cubeDataFrame.schema}")
                log.error(s"Head element. ${cubeDataFrame.head}")
                log.error(s"Error message : ${e.getMessage}")
            }
            case None => log.warn(s"The output in the cube : $outputName not match in the outputs")
          })

        writeTriggers(cubeDataFrame, cube.triggers, tableSchema.tableName, triggerSchemas, triggerOutputs)
      } else log.debug("Empty event received")
    })
  }

  def toRow(dimensionValuesT: DimensionValuesTime, measures: MeasuresValues): Row = {
    val measuresSorted = measuresValuesSorted(measures.values ++ options.fixedMeasures.values)
    val rowValues = dimensionValuesT.timeConfig match {
      case None =>
        val dimensionValues = dimensionValuesWithId(dimensionsValuesSorted(dimensionValuesT.dimensionValues))

        dimensionValues ++ measuresSorted
      case Some(timeConfig) =>
        val timeValue = Seq(timeFromDateType(timeConfig.eventTime, options.dateType))
        val dimFilteredByTime = filterDimensionsByTime(dimensionValuesT.dimensionValues, timeConfig.timeDimension)
        val dimensionValues = dimensionValuesWithId(dimensionsValuesSorted(dimFilteredByTime) ++ timeValue)
        val measuresValuesWithTime = measuresSorted

        dimensionValues ++ measuresValuesWithTime
    }

    Row.fromSeq(rowValues)
  }

  private def dimensionsValuesSorted(dimensionValues: Seq[DimensionValue]): Seq[Any] =
    dimensionValues.sorted.map(dimVal => dimVal.value)

  private def measuresValuesSorted(measures: Map[String, Option[Any]]): Seq[Any] =
    measures.toSeq.sortWith(_._1 < _._1).map(measure => measure._2.getOrElse(0))

  private def dimensionValuesWithId(values: Seq[Any]): Seq[Any] =
    if (options.isAutoCalculatedId) Seq(values.mkString(Output.Separator)) ++ values
    else values

  private def filterDimensionsByTime(dimensionValues: Seq[DimensionValue], timeDimension: String): Seq[DimensionValue] =
    dimensionValues.filter(dimensionValue => dimensionValue.dimension.name != timeDimension)

  private def timeFromDateType[T](time: Long, dateType: TypeOp.Value): Any = {
    dateType match {
      case TypeOp.Date | TypeOp.DateTime => new Date(time)
      case TypeOp.Long => time
      case TypeOp.Timestamp => new Timestamp(time)
      case _ => time.toString
    }
  }
}

object CubeWriter {

  final val FixedMeasureSeparator = ":"
}