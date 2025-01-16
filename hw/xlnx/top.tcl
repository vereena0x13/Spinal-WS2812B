set hwDir [file normalize ..]
set verilogDir [file normalize $hwDir/verilog]
set genDir [file normalize $hwDir/gen]
set xlnxDir [file normalize $hwDir/xlnx]


set_part xc7a100tcsg324-1


read_verilog $genDir/FedUp.v
read_verilog $verilogDir/top.v
read_verilog $verilogDir/synchronizer.v
read_verilog $verilogDir/soc_mmcm.v


read_xdc $xlnxDir/top.xdc


synth_design -top top


opt_design
place_design
route_design


write_bitstream -force top.bit