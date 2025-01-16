import sys
import serial
import signal


should_exit = False


def signal_handler(sig, frame):
    global should_exit
    should_exit = True


def main():
    if len(sys.argv) < 2:
        print("usage: %s <uart>" % sys.argv[0])
        sys.exit(1)

    signal.signal(signal.SIGINT, signal_handler)

    dev = serial.Serial(sys.argv[1], 3000000)

    global should_exit
    while not should_exit:
        x = int.from_bytes(dev.read(1))
        sys.stdout.write(chr(x))
        sys.stdout.flush()
    
    dev.close()


if __name__ == "__main__":
    main()