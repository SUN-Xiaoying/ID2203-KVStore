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
package se.kth.id2203.simulation.operation

import org.scalatest._
import se.kth.id2203.ParentComponent
import se.kth.id2203.networking._
import se.kth.id2203.simulation.ScenarioClient
import se.kth.id2203.simulation.failure.SimpleScenario.{intToClientAddress, intToServerAddress}
import se.kth.id2203.simulation.failure.SimpleScenario
import se.sics.kompics.network.Address
import se.sics.kompics.simulator.adaptor.Operation1
import se.sics.kompics.simulator.events.system.StartNodeEvent

import java.net.{InetAddress, UnknownHostException}
import se.sics.kompics.sl._
import se.sics.kompics.sl.simulator._
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import se.sics.kompics.simulator.run.LauncherComp
import se.sics.kompics.simulator.result.SimulationResultSingleton
import se.sics.kompics.simulator.network.impl.NetworkModels

import scala.concurrent.duration._

class OpsTest extends FlatSpec with Matchers {

  private val nMessages = 10;

  //  "Classloader" should "be something" in {
  //    val cname = classOf[SimulationResultSingleton].getCanonicalName();
  //    var cl = classOf[SimulationResultSingleton].getClassLoader;
  //    var i = 0;
  //    while (cl != null) {
  //      val res = try {
  //        val c = cl.loadClass(cname);
  //        true
  //      } catch {
  //        case t: Throwable => false
  //      }
  //      println(s"$i -> ${cl.getClass.getName} has class? $res");
  //      cl = cl.getParent();
  //      i -= 1;
  //    }
  //  }

  "Get default value " should "return <key, key+100>" in {
    val seed = 123L;
    JSimulationScenario.setSeed(seed)
    // val simpleBootScenario = SimpleScenario.scenario(6, SimpleScenario.defaultValueClient);
    val simpleBootScenario = SimpleScenario.scenario(6, SimpleScenario.defaultValueClient);
    SimulationResultSingleton.getInstance();
    SimulationResult += ("getKeyTest" -> nMessages);
    simpleBootScenario.simulate(classOf[LauncherComp]);
    for (i <- 0 to nMessages) {
      SimulationResult.get[String]("message"+i.toString).get shouldBe (10+i).toString;
    }
  }

  "PUT CAS GET operations in the KV-store" should "be linearizable" in {
    val seed = 123L
    JSimulationScenario.setSeed(seed)
    val simpleBootScenario = SimpleScenario.scenario(6, SimpleScenario.putCasGetClient)
    SimulationResultSingleton.getInstance()

    val range = 20 to 30;
    SimulationResult += ("pcgTest" -> "20-30");
    simpleBootScenario.simulate(classOf[LauncherComp]);
    for (i <- range) {
      SimulationResult.get[String]("PCG-"+i.toString).get shouldBe
        // for CAS odd numbers will be swapped to 10x of previous
        (if (i%2 == 0) i.toString else (10*i).toString)
    }
  }

//  "Simple Operations" should "not be implemented" in { // well of course eventually they should be implemented^^
//    val seed = 123l;
//    JSimulationScenario.setSeed(seed);
//    val simpleBootScenario = SimpleScenario.scenario(3, SimpleScenario.startClientOp);
//    val res = SimulationResultSingleton.getInstance();
//    SimulationResult += ("messages" -> nMessages);
//    simpleBootScenario.simulate(classOf[LauncherComp]);
//    for (i <- 0 to nMessages) {
//      SimulationResult.get[String](s"test$i") should be (Some("NotImplemented"))
//    }
//  }

}

object SimpleScenario {

  import Distributions._
  // needed for the distributions, but needs to be initialised after setting the seed
  implicit val random = JSimulationScenario.getRandom();

  private def intToServerAddress(i: Int): Address = {
    try {
      NetAddress(InetAddress.getByName("192.193.0." + i), 45678);
    } catch {
      case ex: UnknownHostException => throw new RuntimeException(ex);
    }
  }
  private def intToClientAddress(i: Int): Address = {
    try {
      NetAddress(InetAddress.getByName("192.193.1." + i), 45678);
    } catch {
      case ex: UnknownHostException => throw new RuntimeException(ex);
    }
  }

  private def isBootstrap(self: Int): Boolean = self == 1;

  val setUniformLatencyNetwork = () => Op.apply((_: Unit) => ChangeNetwork(NetworkModels.withUniformRandomDelay(3, 7)));

  val startServerOp = Op { (self: Integer) =>
    val selfAddr = intToServerAddress(self)
    val conf = if (isBootstrap(self)) {
      // don't put this at the bootstrap server, or it will act as a bootstrap client
      Map("id2203.project.address" -> selfAddr)
    } else {
      Map(
        "id2203.project.address" -> selfAddr,
        "id2203.project.bootstrap-address" -> intToServerAddress(1))
    };
    StartNode(selfAddr, Init.none[ParentComponent], conf);
  };

  val startClientOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1));
    StartNode(selfAddr, Init.none[ScenarioClient], conf);
  };

  val defaultValueClient = Op { self: Integer =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1))
    StartNode(selfAddr, Init.none[KeyGetTest], conf);
  }
  val putCasGetClient = Op { self: Integer =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1))
    StartNode(selfAddr, Init.none[PutCasGetTest], conf);
  }

  def scenario(servers: Int, cl: Operation1[StartNodeEvent, Integer]): JSimulationScenario = {

    val startCluster = raise(servers, startServerOp, 1.toN).arrival(constant(1.second));
    val startClients = raise(1, cl,  1.toN).arrival(constant(1.second));

    startCluster andThen
      10.seconds afterTermination startClients andThen
      100.seconds afterTermination Terminate
  }

}