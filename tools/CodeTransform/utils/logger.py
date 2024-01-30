import logging

logger = logging.getLogger('CodeTransform')   # choose some name for the loggger

debug = False

if debug:
    logger.setLevel(logging.DEBUG)  # defining the threshold level for the logging
else:
    logger.setLevel(logging.INFO)
    
# Adding a console handler to print logs to the console
handler = logging.StreamHandler()
file_handler = logging.FileHandler("log.txt")

# Adding a file handler to write logs to a file
formatter = logging.Formatter('[%(levelname)s] %(asctime)s: %(message)s')
handler.setFormatter(formatter)
file_handler.setFormatter(formatter)

logger.addHandler(handler)
logger.addHandler(file_handler)


