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

package com.stratio.sparkta.sdk.test

import java.io.{Serializable => JSerializable}

import com.stratio.sparkta.sdk.Parser
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

class ParserMock(order: Integer,
                 inputField: String,
                 outputFields: Seq[String],
                 schema: StructType,
                 properties: Map[String, JSerializable])
  extends Parser(order, inputField, outputFields, schema, properties) {

  override def parse(row: Row, removeRaw: Boolean): Row = row
}
