package se.kth.id2203.leader

import se.kth.id2203.networking.NetAddress
import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port
import se.sics.kompics.timer.{ScheduleTimeout, Timeout}

class BallotLeaderElection extends Port {
  indication[BLE_Leader];
}
case class BLE_Leader(leader: NetAddress, ballot: Long) extends KompicsEvent;


case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout);
case class HeartbeatReq(round: Long, highestBallot: Long) extends KompicsEvent;
case class HeartbeatResp(round: Long, ballot: Long) extends KompicsEvent;
case class StartElection(nodes: Set[NetAddress]) extends KompicsEvent
