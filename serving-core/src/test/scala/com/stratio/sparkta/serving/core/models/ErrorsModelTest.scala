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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import com.stratio.sparkta.serving.core.models.ErrorModel

@RunWith(classOf[JUnitRunner])
class ErrorsModelTest extends WordSpec with Matchers {

  val error = new ErrorModel("100", "Error 100", None, None)

  "ErrorModel" should {

    "toString method should return the number of the error and the error" in {
      val res = ErrorModel.toString(error)
      res should be ("""{"i18nCode":"100","message":"Error 100"}""")
    }

    "toError method should return the number of the error and the error" in {
      val res = ErrorModel.toErrorModel(
        """
          |{
          | "i18nCode": "100",
          | "message": "Error 100"
          |}
        """.stripMargin)
      res should be (error)
    }
  }
}
