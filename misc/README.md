# Misc
This folder contains miscellaneous scripts to quicker test the API.

## REST Authentication Wrapper
The main script is `authenticate`. It sends an authorization request and prints the token
in a format understood by `sh`, similar to `ssh-agent`.

To run one of the other scripts please first run

	eval `./authenticate`

E.g. to fetch all elements, you can run

	eval `./authenticate`
	./authorize elements

### Clipboard
If `xsel` is present on the system, `./authenticate` will copy the JWT token to the
clipboard.

## Generate TLS key pairs
In order for JWT to work, a TLS key pair is needed. A key pair can be generated using

	./genkeys
