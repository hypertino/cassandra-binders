package com.hypertino.binders.cassandra

import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.UUID

import com.hypertino.binders.core.Deserializer
import com.hypertino.inflector.naming.Converter

import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._

class Row[C <: Converter : TypeTag](val row: com.datastax.driver.core.Row) extends Deserializer[C] {
  import scala.collection.JavaConverters._

  def fieldName: Option[String] = None

  def isNull = false

  def iterator() : Iterator[FieldDeserializer[C]] = row.getColumnDefinitions.asScala.map { column =>
    new FieldDeserializer[C](column.getName, row)
  }.toIterator
}

class FieldDeserializer[C <: Converter : TypeTag](val name: String, val row: com.datastax.driver.core.Row) extends Deserializer[C] {
  import scala.collection.JavaConverters._
  def fieldName: Option[String] = Some(name)

  def iterator(): Iterator[Deserializer[C]] = ???

  protected def checkNotNull() = if (row.isNull(name)) throw new ColumnValueIsNullException(name)

  def isNull: Boolean = row.isNull(name)

  def readString(): String = {
    checkNotNull()
    row.getString(name)
  }

  def readInt(): Int = {
    checkNotNull()
    row.getInt(name)
  }

  def readLong(): Long = {
    checkNotNull()
    row.getLong(name)
  }

  def readTimestamp(): java.util.Date = {
    checkNotNull()
    row.getTimestamp(name)
  }

  def readBoolean(): Boolean = {
    checkNotNull()
    row.getBool(name)
  }

  def readFloat(): Float = {
    checkNotNull()
    row.getFloat(name)
  }

  def readDouble(): Double = {
    checkNotNull()
    row.getDouble(name)
  }

  def readBytes(): ByteBuffer = {
    checkNotNull()
    row.getBytes(name)
  }

  def readBigInteger(): BigInteger = {
    checkNotNull()
    row.getVarint(name)
  }

  def readBigDecimal(): BigDecimal = {
    checkNotNull()
    row.getDecimal(name)
  }

  def readUUID(): UUID = {
    checkNotNull()
    row.getUUID(name)
  }

  def readInetAddress(): InetAddress = {
    checkNotNull()
    row.getInet(name)
  }

  // collections implementation isn't not very efficient at the moment
  def readList[T: ClassTag](): List[T] = row.getList(name, classTag[T].runtimeClass).asScala.map(x => x.asInstanceOf[T]).toList

  def readSet[T: ClassTag](): Set[T] = row.getSet(name, classTag[T].runtimeClass).asScala.map(x => x.asInstanceOf[T]).toSet

  def readMap[K: ClassTag, V: ClassTag](): Map[K, V] =
    row.getMap(name, classTag[K].runtimeClass, classTag[V].runtimeClass).asScala.map(kv => (kv._1.asInstanceOf[K], kv._2.asInstanceOf[V])).toMap
}
