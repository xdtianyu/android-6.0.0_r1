#!/bin/sh

usage()
{
	echo usage: $0 futility input.img key.vbpubk key.vbprivk output.keyblock output.img
}

cleanup()
{
	rm -f ${EMPTY}
}

if [ "$#" -ne 6 ]; then
	echo ERROR: invalid number of arguments
	usage
	exit 1
fi

futility=$1
input=$2
pubkey=$3
privkey=$4
keyblock=$5
output=$6

EMPTY=$(mktemp /tmp/tmp.XXXXXXXX)
trap cleanup EXIT
echo " " > ${EMPTY}

echo signing ${input} with ${privkey} to generate ${output}
${futility} vbutil_keyblock --pack ${keyblock} --datapubkey ${pubkey} --signprivate ${privkey}
if [ $? -ne 0 ]; then
	echo ERROR: unable to generate keyblock
	exit $?
fi

${futility} vbutil_kernel --pack ${output} --keyblock ${keyblock} --signprivate ${privkey} --version 1 --vmlinuz ${input} --config ${EMPTY} --arch arm --bootloader ${EMPTY} --flags 0x1
if [ $? -ne 0 ]; then
	echo ERROR: unable to sign image
	exit $?
fi

