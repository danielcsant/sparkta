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

package com.stratio.sparkta.sdk

import java.io.{Serializable => JSerializable}

import com.stratio.sparkta.sdk.TypeOp._
import org.apache.spark.Logging
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types._

import scala.util.Try

abstract class Output(keyName: String,
                      version: Option[Int],
                      properties: Map[String, JSerializable],
                      schemas: Seq[TableSchema])
  extends Parameterizable(properties) with Logging {

  def name: String = keyName

  def setup(options: Map[String, String] = Map.empty[String, String]): Unit = {}

  def upsert(dataFrame: DataFrame, options: Map[String, String]): Unit

  def versionedTableName(tableName: String): String = {
    val versionChain = version match {
      case Some(v) => s"${Output.Separator}v$v"
      case None => ""
    }
    s"$tableName$versionChain"
  }
}

object Output extends Logging {

  final val ClassSuffix = "Output"
  final val Separator = "_"
  final val Id = "id"
  final val FieldsSeparator = ","
  final val TableNameKey = "tableName"
  final val TimeDimensionKey = "timeDimension"
  final val IdAutoCalculatedKey = "idAutoCalculated"

  def getTimeFromOptions(options: Map[String, String]): Option[String] = options.get(TimeDimensionKey)

  def getTableNameFromOptions(options: Map[String, String]): String =
    options.getOrElse(TableNameKey, {
      log.error("Table name not defined")
      throw new NoSuchElementException("tableName not found in options")
    })

  def getIsAutoCalculatedIdFromOptions(options: Map[String, String]): Boolean =
    Try(options.getOrElse(IdAutoCalculatedKey, {
      log.error("Autocalculated not defined")
      throw new NoSuchElementException("Autocalculated not found in options")
    }).toBoolean).getOrElse(throw new NoSuchElementException("Autocalculated with ilegal value"))

  def getTimeFieldType(dateTimeType: TypeOp, fieldName: String, nullable: Boolean): StructField =
    dateTimeType match {
      case TypeOp.Date | TypeOp.DateTime => defaultDateField(fieldName, nullable)
      case TypeOp.Timestamp => defaultTimeStampField(fieldName, nullable)
      case TypeOp.Long => defaultLongField(fieldName, nullable)
      case TypeOp.String => defaultStringField(fieldName, nullable)
      case _ => defaultStringField(fieldName, nullable)
    }

  def defaultTimeStampField(fieldName: String, nullable: Boolean): StructField =
    StructField(fieldName, TimestampType, nullable)

  def defaultDateField(fieldName: String, nullable: Boolean): StructField =
    StructField(fieldName, DateType, nullable)

  def defaultStringField(fieldName: String, nullable: Boolean): StructField =
    StructField(fieldName, StringType, nullable)

  def defaultGeoField(fieldName: String, nullable: Boolean): StructField =
    StructField(fieldName, ArrayType(DoubleType), nullable)

  def defaultLongField(fieldName: String, nullable: Boolean): StructField =
    StructField(fieldName, LongType, nullable)
}
