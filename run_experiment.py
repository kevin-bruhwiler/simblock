from subprocess import check_output
from collections import defaultdict
import ast
import matplotlib.pyplot as plt
import numpy as np


def download_with_value():
    return ["{{25000000, {0}, 18000000, {0}, {0}, {0}, {0}}}", "{{{0}, 40000000, 18000000, 22800000, 22800000, 29900000}}", "{{18000000, 18000000, 18000000, 18000000, 18000000, 18000000}}", "{{{0}, 22800000, 18000000, 22800000, 22800000, 22800000}}", "{{{0}, 22800000, 18000000, 22800000, 22800000, 22800000}}", "{{{0}, 29900000, 18000000, 22800000, 22800000, 29900000}}"]


def upload_with_value():
    return ["{{19200000, {0}, 5800000, {0}, {0}, {0}, {0}}}", "{{{0}, 20700000, 5800000, 15700000, 10200000, 11300000}}", "{{5800000, 5800000, 5800000, 5800000, 5800000, 5800000}}", "{{{0}, 15700000, 5800000, 15700000, 10200000, 11300000}}", "{{{0}, 10200000, 5800000, 10200000, 10200000, 10200000}}", "{{{0}, 11300000, 5800000, 11300000, 10200000, 11300000}}"]


with open('D:\\simblock\\simulator\\src\\main\\java\\simblock\\settings\\NetworkConfiguration.java', 'r') as f:
    settings = f.read()

throttles = [1000, 10000000, 1000000, 100000, 10000, 1000]
data = defaultdict(list)
block_counts = defaultdict(list)

for i in range(1, len(throttles)):
	for l in download_with_value():
		settings = settings.replace(l.format(throttles[i-1]), l.format(throttles[i]))

	for l in upload_with_value():
		settings = settings.replace(l.format(throttles[i-1]), l.format(throttles[i]))

	with open('D:\\simblock\\simulator\\src\\main\\java\\simblock\\settings\\NetworkConfiguration.java', 'w') as f:
		f.write(settings)

	for _ in range(3):
		out = check_output("gradlew.bat :simulator:run", shell=True).decode()
		for line in out.split('\n'):
			if line.startswith("Number of detected forks:"):
				data[i].append(int(line.split(' ')[-1]))
			elif line.startswith("Count of blocks at each node: "):
				block_counts[throttles[i]].append(ast.literal_eval(line.split(': ')[-1]))
			
print(data)
plt.errorbar([throttles[x] for x in data.keys()], 
			 [np.mean(x) for _, x in data.items()],
			 [np.std(x) for _, x in data.items()])
plt.xscale('log')
plt.xlabel("Bandwidth (bit/sec)")
plt.ylabel("Number of Forks")
plt.title("NA Maintains Connections to SA")
plt.show()
plt.clf()

print(block_counts)
for k, v in block_counts.items():
	plt.plot(np.mean(np.array(v), axis=0), label="Bandwidth of " + str(k) + " bits/sec")
plt.legend()
plt.ylabel("Total Blocks Added")
plt.xlabel("Index of the Block")
plt.title("Total Blocks Added in Superstorm Scenario")
plt.show()
