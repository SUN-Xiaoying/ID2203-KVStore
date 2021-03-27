/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.kvstore


import se.kth.id2203.sc._
import se.kth.id2203.networking._
import se.kth.id2203.overlay.Routing
import se.sics.kompics.network.Network
import se.sics.kompics.sl._

import scala.collection.mutable;

class KVService extends ComponentDefinition {

  //******* Ports ******
  val net: PositivePort[Network] = requires[Network];
  val route: PositivePort[Routing.type] = requires(Routing);
  val sc: PositivePort[SequenceConsensus] = requires[SequenceConsensus];

  //******* Fields ******
  val self: NetAddress = cfg.getValue[NetAddress]("id2203.project.address");

  // members
  val KVStore = mutable.Map.empty[String, String]

  for (i <- 0 to 10) {
    KVStore += ((i.toString, (i+10).toString))
  }

  //******* Handlers ******
  net uponEvent {
    case NetMessage(_, op: Op) => {
      trigger(SC_Propose(op) -> sc);
    }
  }

  sc uponEvent {
    case SC_Decide(op: Get) => {
      println(s"KVService GET $op")
      if(KVStore.contains(op.key)){
        val value = Some(KVStore(op.key))
        trigger(NetMessage(self, op.src, op.response(OpCode.Ok, value)) -> net)
      }else{
        trigger(NetMessage(self, op.src, op.response(OpCode.NotFound, Some("NotFound"))) -> net)
      }
    }

    case SC_Decide(op: Put) => {
      println(s"KVService PUT $op")
      KVStore += ((op.key, op.value))
      trigger(NetMessage(self, op.src, op.response(OpCode.Ok, Some(op.value))) -> net);
    }

    case SC_Decide(op: Cas) => {
      println(s"KVService CAS $op")
      if(!KVStore.contains(op.key)){
        println("CAS KEY " +op.key+" Not Exist!")
        trigger(NetMessage(self, op.src, op.response(OpCode.NotFound, Some("NotFound"))) -> net)
      }else{
        val currentValue = KVStore(op.key)
        println("CAS current: "+currentValue+", given: " +op.refValue)
        if(currentValue != op.refValue){
          println("CAS Compare: NOT MATCH")
          trigger(NetMessage(self, op.src, op.response(OpCode.NotImplemented, Some(currentValue))) -> net)
        }else{
          KVStore += (op.key -> op.newValue)
          log.info(s"CAS Completed")
          trigger(NetMessage(self, op.src, op.response(OpCode.Ok, Some(op.newValue))) -> net)
        }
      }
    }
  }
}
