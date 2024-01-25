#!/bin/bash
eval "$(conda shell.bash hook)"
conda activate cohabit
python server.py 2>&1 | tee -a server-$(date +"%Y-%m-%d").log

