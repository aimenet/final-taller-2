from math import sqrt
from PIL import Image


reference_image_path = "/home/rodrigo/Pictures/sdri/lago.jpg"
image_paths = [
	"/home/rodrigo/Pictures/sdri/lago2.jpeg",
	"/home/rodrigo/Pictures/sdri/lago3.jpeg",
	"/home/rodrigo/Pictures/sdri/lago4.jpeg",
	"/home/rodrigo/Pictures/sdri/lago (copia).jpg",
	"/home/rodrigo/Pictures/sdri/avion.png",
	"/home/rodrigo/Pictures/sdri/doom.jpg",
	"/home/rodrigo/Pictures/sdri/desert.jpeg",
	"/home/rodrigo/Pictures/sdri/forest.jpeg",
	"/home/rodrigo/Pictures/sdri/xray.jpg",
	"/home/rodrigo/Pictures/sdri/car.jpeg",
]


def calc_histogram(image):
	histo = [0 for _ in range(256 * 3)]
	columns, rows = image.size
	pixels = rows * columns

	red_zero_index = 0
	green_zero_index = 256
	blue_zero_index = 512

	for row in range(rows):
		for column in range(columns):
			pixel_data = image.getpixel((column, row))

			if len(pixel_data) == 3:
				red, green, blue = pixel_data
			elif len(pixel_data) == 4:
				red, green, blue, _ = pixel_data

			red_index = red_zero_index + red
			green_index = green_zero_index + green
			blue_index = blue_zero_index + blue

			histo[red_index] += 1
			histo[green_index] += 1
			histo[blue_index] += 1

	for i in range(len(histo)):
		histo[i] /= pixels * 1.0

	return histo


def calc_compressed_histogram(histo):
	bins = 12
	compressed_histo = [0 for _ in range(bins)]
	values_per_bin = len(histo) / bins;

	for i in range(len(histo)):
		count = histo[i]
		index = i / values_per_bin

		compressed_histo[index] += count;

	return compressed_histo


def calc_euclidean_distance(histo_a, histo_b):
	distance = 0.0
	bins = len(histo_a)

	for i in range(bins):
		distance += abs(histo_a[i] - histo_b[i]) ** 2
	
	return sqrt(distance)


def calc_distance_two(histo_a, histo_b):
	distance = 0.0
	bins = len(histo_a)

	for i in range(bins):
		distance += abs(histo_a[i] - histo_b[i]) / float(255.0)
	
	return distance / float(bins)


if __name__ == "__main__":
	reference_image = Image.open(reference_image_path)
	reference_histo = calc_histogram(reference_image)
	reference_compressed_histo = calc_compressed_histogram(reference_histo)

	for image_path in image_paths:
		image = Image.open(image_path)
		histo = calc_histogram(image)
		compressed_histo = calc_compressed_histogram(histo)
		euclidean_distance = calc_euclidean_distance(reference_histo, histo)
		distance_two = calc_euclidean_distance(reference_compressed_histo, compressed_histo)

		print("Image: " + image_path)
		print("Euclidean Distance: {:f}".format(euclidean_distance))
		print("Distance Two: {:f}".format(distance_two))
		print("")
