NAME=top
OPENOCD_CFG=neso.cfg
REPORT_DIR=rpt
REPORT_LAST_DIR=$(REPORT_DIR)/last

.PHONY: clean synth program flash modftdi

clean:
	rm -f $(NAME).log
	rm -f $(NAME).bit
	rm -f rpt/*.rpt
	rm -f rpt/last/*.rpt

synth:
	rm -f $(NAME).log
	rm -f $(NAME).bit
	mkdir -p $(REPORT_LAST_DIR)
	rm -f $(REPORT_LAST_DIR)/*.rpt
	find $(REPORT_DIR) -type f -exec mv {} $(REPORT_LAST_DIR) \;
	vivado -mode batch -log $(NAME).log -nojournal -source $(NAME).tcl

program:
	openocd -f $(OPENOCD_CFG) -c "init" -c "pld load 0 $(NAME).bit" -c "shutdown"

flash:
	openocd -f $(OPENOCD_CFG) -c "init" -c "jtagspi_init 0 bscan_spi_xc7a100t.bit" -c "jtagspi_program $(NAME).bin 0" -c "xc7_program xc7.tap" -c "shutdown"

modftdi:
	sudo su -c "modprobe ftdi_sio && echo 2a19 1005 > /sys/bus/usb-serial/drivers/ftdi_sio/new_id"