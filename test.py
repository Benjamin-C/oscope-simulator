import numpy as np
import matplotlib.pyplot as plt
import struct 

data = np.genfromtxt('log.csv', delimiter=',')

print(np.shape(data))

fig,ax = plt.subplots()

x = []
y = []

for row in data:
    tx, ty = struct.unpack("<hh", row[0:4].astype(np.int8).tobytes())
    x.append(tx)
    y.append(ty)

ax.plot(data[:,4], data[:,5], ".", color="blue")
ax.plot(x, y, ".", color="orange")

plt.show()