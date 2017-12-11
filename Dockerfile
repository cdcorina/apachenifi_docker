FROM mkobit/nifi:latest
ADD ./nifi.properties conf
ADD ./start-nifi.sh .

CMD ["./start-nifi.sh"]
