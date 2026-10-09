all: compile_cpp_bnfc test-all

compile_cpp_bnfc:
	bash ./compile_cpp_bnfc.sh

test-all:
	ls -1 -d ./samples/* | xargs -ifrag ./run_tccpp.sh frag


