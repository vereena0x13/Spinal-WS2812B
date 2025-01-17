import sys
import serial
import signal
import time


should_exit = False


def signal_handler(sig, frame):
    global should_exit
    should_exit = True


def main():
    if len(sys.argv) < 2:
        print("usage: %s <uart>" % sys.argv[0])
        sys.exit(1)

    signal.signal(signal.SIGINT, signal_handler)

    dev = serial.Serial(sys.argv[1], 3_000_000)
    time.sleep(0.5)


    def set(x, y, r, g, b):
        dev.write(bytearray([x, y, r, g, b]))
        dev.flush()

    for x in range(16):
        for y in range(16):
            set(x, y, x, y, 0)
            
    
    dev.close()



if __name__ == "__main__":
    main()