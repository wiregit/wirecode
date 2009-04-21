#include <stdio.h>
#include <stdlib.h>
#include <iostream>

extern "C" void print() {
	std::cout << "print called!" << std::endl;
}

int main(int argc, char* argv[]) {
	std::cout << "testing!" << std::endl;
	print();
	return 0;
}

