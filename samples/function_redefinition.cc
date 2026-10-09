int add(int x, int y) {
	return x + y;
}

// Our CPP fragment does not support overloading.
int add(double x, double y) {
	return x + y;
}


int main(int x, int y) {
	add(x, y);
	return 0;
}
