f = open("c_512.txt", "r")
lines = f.readlines()
gflops = []
sizes = []
times = []
percs = []

for line in lines:
    parsed = line.split()
    size = int(parsed[0])
    time = float(parsed[2])
    dcm = int(parsed[4])
    dch = int(parsed[6])

    gflop = 2 * (size**3) / time
    perc = (dcm / (dcm + dch)) * 100

    gflops.append(gflop)
    sizes.append(size)
    times.append(time)
    percs.append(perc)


w = open("out.txt", "w")
for i in range(len(gflops)):
    w.write(str(sizes[i]) + " - " + str(times[i]) + " - " + str(gflops[i]) + " - " + str(percs[i]) + "\n")

f.close()
w.close()