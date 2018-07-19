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
