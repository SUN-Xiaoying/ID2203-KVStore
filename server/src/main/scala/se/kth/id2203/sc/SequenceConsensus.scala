package se.kth.id2203.sc

import se.kth.id2203.kvstore.Op
import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port
import se.kth.id2203.networking.NetAddress

class SequenceConsensus extends Port {
  request[SC_Propose];
  indication[SC_Decide];
  request[StartSequenceCons];
}

case class Prepare(nL: Long, ld: Int, na: Long) extends KompicsEvent;
case class Promise(nL: Long, na: Long, suffix: List[Op], ld: Int) extends KompicsEvent;
case class AcceptSync(nL: Long, suffix: List[Op], ld: Int) extends KompicsEvent;
case class Accept(nL: Long, c: Op) extends KompicsEvent;
case class Accepted(nL: Long, m: Int) extends KompicsEvent;
case class Decide(ld: Int, nL: Long) extends KompicsEvent;
case class StartSequenceCons(nodes: Set[NetAddress]) extends KompicsEvent;
case class SC_Propose(value: Op) extends KompicsEvent;
case class SC_Decide(value: Op) extends KompicsEvent;

object State extends Enumeration {
  type State = Value;
  val PREPARE, ACCEPT, UNKOWN = Value;
}

object Role extends Enumeration {
  type Role = Value;
  val LEADER, FOLLOWER = Value;
}