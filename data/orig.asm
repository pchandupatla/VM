; moo
.ORIG 0x3000 ; does stuff
MOV ANSHL, 0x5
LABEL MOV SPNCR, 0x5
MOV ANSHL, LABEL
LEAP SPNCR, 0x2004
DSR SPNCR
RSCOOTA SPNCR, 0x3
.END
