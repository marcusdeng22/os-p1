import os

for file in os.listdir(os.path.dirname(os.path.realpath(__file__))):
	#remove all .class files
	if os.path.isfile(file) and ".class" in file:
		print("removing file:", file)
		os.unlink(file)

for folder in ["0", "1", "2"]:
	#remove all files in folders
	for file in os.listdir(folder):
		path = os.path.join(folder, file)
		try:
			if os.path.isfile(path):
				print("file removed @", path)
				os.unlink(path)
		except Exception as e:
			print(e)

	#generate a few files
	for tempFile in ["abc", "def"]:
		with open(os.path.join(folder, tempFile), "w") as fd:
			fd.write(tempFile + "file\n")
