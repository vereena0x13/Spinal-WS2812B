NAME=top
OPENOCD_CFG=neso.cfg

.PHONY: clean synth program modftdi

clean:
	rm -f $(NAME).log
	rm -f $(NAME).bit

synth: clean
	vivado -mode batch -log $(NAME).log -nojournal -source $(NAME).tcl

program:
	openocd -f $(OPENOCD_CFG) -c "init" -c "pld load 0 $(NAME).bit" -c "shutdown"

modftdi:
	sudo su -c "modprobe ftdi_sio && echo 2a19 1005 > /sys/bus/usb-serial/drivers/ftdi_sio/new_id"