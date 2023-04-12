package nucleusrv.components
import chisel3._
import nucleusrv.tracer._
import caravan.bus.common.{AbstrRequest, AbstrResponse, BusConfig, BusDevice, BusHost}
import caravan.bus.tilelink.{TLRequest, TLResponse, TilelinkConfig, TilelinkAdapter}


class Top(programFile:Option[String], dataFile:Option[String]) extends Module{

  val io = IO(new Bundle() {
    val pin = Output(UInt(32.W))
  })

  implicit val config1:Configs = Configs(XLEN=32, M=true, C=true, TRACE=false)
  implicit val config =TilelinkConfig()

  val core: Core = Module(new Core())
  core.io.stall := false.B

  val imemAdapter = Module(new TilelinkAdapter()) //instrAdapter
  val dmemAdapter = Module(new TilelinkAdapter()) //dmemAdapter

  val dmem = Module(new SRamTop(dataFile))
  val imem = Module(new SRamTop(programFile))

  /*  Imem Interceonnections  */
  imemAdapter.io.reqIn <> core.io.imemReq
  core.io.imemRsp <> imemAdapter.io.rspOut
  imem.io.req <> imemAdapter.io.reqOut
  imemAdapter.io.rspIn <> imem.io.rsp

  /*  Dmem Interconnections  */
  dmemAdapter.io.reqIn <> core.io.dmemReq
  core.io.dmemRsp <> dmemAdapter.io.rspOut
  dmem.io.req <> dmemAdapter.io.reqOut
  dmemAdapter.io.rspIn <> dmem.io.rsp

  io.pin := core.io.pin

  if (config1.TRACE) {
    val tracer = Module(new Tracer())

    Seq(
      (tracer.io.rvfiUInt, core.io.rvfiUInt.get),
      (tracer.io.rvfiSInt, core.io.rvfiSInt.get),
      (tracer.io.rvfiBool, core.io.rvfiBool.get),
      (tracer.io.rvfiRegAddr, core.io.rvfiRegAddr.get)
    ).map(
      tr => tr._1 <> tr._2
    )
    tracer.io.rvfiMode := core.io.rvfiMode.get
  }
}