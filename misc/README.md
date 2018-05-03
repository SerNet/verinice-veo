# Misc
This folder contains miscellaneous scripts to quicker test the API.

The main script is `authenticate`. It sends an authorization request and prints the token
in a format understood by `sh`, similar to `ssh-agent`.

To run one of the other scripts please first run

	eval `./authenticate`

E.g. to fetch all elements, you can run

	eval `./authenticate`
	./authorize elements

