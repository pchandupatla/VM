; moo
.ORIG 0x3000 ; does stuff
MOV ANSHL, 0x3000
ADD ANSHL, ANSHL, 0x5
MOV ANSHL, 0x5
LABEL MOV SPNCR, 0x5
MOV PRNV, 0x6
MOV ANSHL, LABEL
DSR SPNCR
RSCOOTA SPNCR, 0x3
.END
