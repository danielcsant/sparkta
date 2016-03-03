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

package com.stratio.sparkta.plugin.parser.morphline

import java.io.{ByteArrayInputStream, Serializable => JSerializable}
import java.util.concurrent.ConcurrentHashMap

import com.stratio.sparkta.sdk.Parser
import com.stratio.sparkta.sdk.ValidatingPropertyMap._
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StructField, StructType}
import org.kitesdk.morphline.api.Record

import scala.collection.JavaConverters._

class MorphlinesParser(order: Integer,
                       inputField: String,
                       outputFields: Seq[String],
                       schema: StructType,
                       properties: Map[String, JSerializable])
  extends Parser(order, inputField, outputFields, schema, properties) {

  private val config: String = properties.getString("morphline")

  override def parse(row: Row, removeRaw: Boolean): Row = {
    val record = new Record()
    val inputValue = row.get(inputFieldIndex)
    val result = inputValue match {
      case s: String => new ByteArrayInputStream(s.getBytes("UTF-8"))
      case b: Array[Byte] => new ByteArrayInputStream(b)
    }

    record.put(inputField, result)

    val morphlineResult = MorphlinesParser(order, config, outputFieldsSchema).process(record)
    val prevData = if (removeRaw) Row.fromSeq(row.toSeq.drop(1)) else row

    Row.merge(prevData, morphlineResult)
  }
}

object MorphlinesParser {

  private val instances = new ConcurrentHashMap[String, KiteMorphlineImpl].asScala

  def apply(order: Integer, config: String, outputFieldsSchema: Array[StructField]): KiteMorphlineImpl = {
    instances.get(config) match {
      case Some(kiteMorphlineImpl) =>
        kiteMorphlineImpl
      case None =>
        val kiteMorphlineImpl = KiteMorphlineImpl(ConfigFactory.parseString(config), outputFieldsSchema)
        instances.putIfAbsent(config, kiteMorphlineImpl)
        kiteMorphlineImpl
    }
  }
}
