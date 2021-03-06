/*
 * Copyright 2009-2010 LinkedIn, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.linkedin.norbert.cluster

import scala.reflect.BeanProperty
import com.google.protobuf.InvalidProtocolBufferException
import com.linkedin.norbert.protos.NorbertProtos

/**
 * The <code>Node</code> companion object. Provides factory methods and implicits.
 */
object Node {
  
  /**
   * Creates a <code>Node</code> instance with no partitions assigned to it.
   *
   * @param id the id of the node
   * @param address the <code>InetSocketAddress</code> on which requests can be sent to the node
   * @param partitions the partitions for which the node can handle requests
   * @param available whether or not the node is currently able to process requests
   *
   * @return a new <code>Node</code> instance
   */
  def apply(id: Int, url: String, available: Boolean): Node = apply(id, url, new Array[Int](0), available)

  /**
   * Creates a <code>Node</code> instance using the serialized state of a node.
   *
   * @param id the id of the node
   * @param bytes the serialized state of the a node.  <code>Node</code>s should be serialized using the
   * <code>nodeToByteArray</code> implicit implemented in this object.
   * @param available whether or not the node is currently able to process requests
   *
   * @return a new <code>Node</code> instance
   */
  def apply(id: Int, bytes: Array[Byte], available: Boolean): Node = {
    import collection.jcl.Conversions._

    try {
      val node = NorbertProtos.Node.newBuilder.mergeFrom(bytes).build
      val partitions = new Array[Int](node.getPartitionCount)
      node.getPartitionList.asInstanceOf[java.util.List[Int]].copyToArray(partitions, 0)

      Node(node.getId, node.getUrl, partitions, available)
    } catch {
      case ex: InvalidProtocolBufferException => throw new InvalidNodeException("Error deserializing node", ex)
    }
  }

  /**
   * Implicit method which serializes a <code>Node</code> instance into an array of bytes.
   *
   * @param node the <code>Node</code> to serialize
   *
   * @return the serialized <code>Node</code>
   */
  implicit def nodeToByteArray(node: Node): Array[Byte] = {
    val builder = NorbertProtos.Node.newBuilder
    builder.setId(node.id).setUrl(node.url)
    node.partitions.foreach(builder.addPartition(_))

    builder.build.toByteArray
  }
}

/**
 * A representation of a physical node in the cluster.
 *
 * @param id the id of the node
 * @param address the <code>InetSocketAddress</code> on which requests can be sent to the node
 * @param partitions the partitions for which the node can handle requests
 * @param available whether or not the node is currently able to process requests
 */
final case class Node(@BeanProperty id: Int, @BeanProperty url: String,
        @BeanProperty partitions: Array[Int], @BeanProperty available: Boolean) {
  if (url == null) throw new NullPointerException("url must not be null")
  if (partitions == null) throw new NullPointerException("partitions must not be null")
  
  override def hashCode = id.hashCode

  override def equals(other: Any) = other match {
    case that: Node => this.id == that.id && this.url == that.url
    case _ => false
  }

  override def toString = "Node(%d,%s,[%s],%b)".format(id, url, partitions.mkString(","), available)
}
