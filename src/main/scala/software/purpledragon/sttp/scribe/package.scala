/*
 * Copyright 2018 Michael Stringer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.purpledragon.sttp

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}

package object scribe {
  private[sttp] def transfer(is: InputStream, os: OutputStream): Unit = {
    val buffer = new Array[Byte](1024)

    Iterator
      .continually(is.read(buffer))
      .takeWhile(_ != -1)
      .foreach(count => os.write(buffer, 0, count))
  }

  private[sttp] def toByteArray(is: InputStream): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    transfer(is, baos)
    baos.toByteArray
  }


}
