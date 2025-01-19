set_property CFGBVS VCCO [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]


set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN F4 } [get_ports {t_clk}]
create_clock -name t_clk -period 10.0 [get_ports {t_clk}]
set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN A14 PULLUP TRUE } [get_ports {t_rst}]


set_property IOSTANDARD LVCMOS33 [get_ports {t_uart_data[*]}]
set_property PACKAGE_PIN J18 [get_ports {t_uart_data[7]}]
set_property PACKAGE_PIN J17 [get_ports {t_uart_data[6]}]
set_property PACKAGE_PIN G18 [get_ports {t_uart_data[5]}]
set_property PACKAGE_PIN F18 [get_ports {t_uart_data[4]}]
set_property PACKAGE_PIN E18 [get_ports {t_uart_data[3]}]
set_property PACKAGE_PIN D18 [get_ports {t_uart_data[2]}]
set_property PACKAGE_PIN B18 [get_ports {t_uart_data[1]}]
set_property PACKAGE_PIN A18 [get_ports {t_uart_data[0]}]

set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN K16 } [get_ports {t_uart_txe}]
set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN G13 } [get_ports {t_uart_rxf}]
set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN M13 } [get_ports {t_uart_wr}]
set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN D9 } [get_ports {t_uart_rd}]


set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN A13  LOC IOB_X0Y131  SLEW FAST } [get_ports {t_gpio_a13}]