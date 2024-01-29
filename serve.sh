#!/bin/bash
lein run 2>&1 | tee -a server-$(date +"%Y-%m-%d").log
