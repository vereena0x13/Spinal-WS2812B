# Voltage Config
set_property CFGBVS VCCO [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]


# Clock and Reset
set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN F4 } [get_ports {t_clk}]
create_clock -name t_clk -period 10.0 [get_ports {t_clk}]
set_property -dict { IOSTANDARD LVCMOS33  PACKAGE_PIN A14  PULLUP TRUE } [get_ports {t_rst}]


# UART Ports
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


# LED Strip Data
set_property -dict { IOSTANDARD LVCMOS33  LOC A13  SLEW FAST } [get_ports {t_gpio_strip_dout}]


# LED Matrices Data
set_property -dict { IOSTANDARD LVCMOS33  LOC H4  SLEW FAST } [get_ports {t_gpio_matrix_dout}]


# Rotary Encoder
set_property -dict { IOSTANDARD LVCMOS33  LOC G3  SLEW FAST } [get_ports {t_gpio_enc_key}]
set_property -dict { IOSTANDARD LVCMOS33  LOC G2  SLEW FAST } [get_ports {t_gpio_enc_b}]
set_property -dict { IOSTANDARD LVCMOS33  LOC B4  SLEW FAST } [get_ports {t_gpio_enc_a}]


# TIL311
set_property -dict { IOSTANDARD LVCMOS33  SLEW FAST } [get_ports {t_gpio_til311_data[*]}]
set_property LOC A11 [get_ports {t_gpio_til311_data[3]}]
set_property LOC B14 [get_ports {t_gpio_til311_data[2]}]
set_property LOC A9 [get_ports {t_gpio_til311_data[1]}]
set_property LOC A8 [get_ports {t_gpio_til311_data[0]}]
set_property -dict { IOSTANDARD LVCMOS33  LOC B9  SLEW FAST } [get_ports {t_gpio_til311_strobe}]
set_property -dict { IOSTANDARD LVCMOS33  LOC C5  SLEW FAST } [get_ports {t_gpio_til311_blank}]


# Bitstream Config
set_property BITSTREAM.GENERAL.COMPRESS FALSE [current_design]
set_property BITSTREAM.CONFIG.CONFIGRATE 12 [current_design]
set_property BITSTREAM.CONFIG.SPI_32BIT_ADDR NO [current_design]
set_property BITSTREAM.CONFIG.SPI_BUSWIDTH 4 [current_design]
set_property BITSTREAM.CONFIG.SPI_FALL_EDGE YES [current_design]