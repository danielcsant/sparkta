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

package com.stratio.sparkta.serving.core.models

import com.stratio.sparkta.sdk.{Parser, Input, JsoneyString}
import com.stratio.sparkta.serving.core.constants.AppConstant

case class TransformationsModel(`type`: String,
                                order: Integer,
                                inputField: String = Input.RawDataKey,
                                outputFields: Seq[OutputFieldsModel],
                                configuration: Map[String, JsoneyString] = Map()) {

  val outputFieldsTransformed = outputFields.map(field =>
    OutputFieldsTransformedModel(field.name,
      field.`type`.getOrElse(Parser.TypesFromParserClass.getOrElse(`type`.toLowerCase, Parser.DefaultOutputType))
    ))
}

case class OutputFieldsModel(name: String, `type`: Option[String] = None)

case class OutputFieldsTransformedModel(name: String, `type`: String)
