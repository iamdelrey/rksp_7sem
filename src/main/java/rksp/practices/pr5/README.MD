### Сборка и запуск кластера

```bash
cd ~/Desktop/java_projects/rksp_7sem
docker compose up --build -d
docker compose ps
```

Ожидаемо видим 5 контейнеров: `app1..app4` и `proxy` (порт `80:80` у proxy).

---

### Быстрый sanity-check (балансировка)

Короткие соединения, чтобы увидеть распределение запросов:

```bash
for i in {1..8}; do curl -s -H 'Connection: close' http://localhost/api/whoami; echo; done
```

Ожидаемо — ответы от разных инстансов (`app1/app2/app3/app4` вперемешку).

---

### Загрузка и список файлов

Создать файлы и загрузить:

```bash
echo "hello dds" > /tmp/file1.txt
dd if=/dev/urandom of=/tmp/random.bin bs=1M count=2
curl -s -F "files=@/tmp/file1.txt" -F "files=@/tmp/random.bin" http://localhost/api/upload
```

Посмотреть список:

```bash
curl -s http://localhost/api/files
```

Скачать первый из списка (без jq):

```bash
NAME=$(curl -s http://localhost/api/files | sed -E 's/.*"name":"([^"]+)".*/\1/;q')
curl -OJ "http://localhost/upload-files/$NAME"
```

*(Если есть `jq`, скачать все:)*

```bash
for n in $(curl -s http://localhost/api/files | jq -r '.[].name'); do
  curl -s -OJ "http://localhost/upload-files/$n"
done
```

---

### Общий volume — проверка на разных инстансах

```bash
docker compose exec app1 ls -l /upload-files
docker compose exec app3 ls -l /upload-files
```

Списки файлов должны совпадать (общий том `uploads`).

---

### Отказоустойчивость (остановка части инстансов)

Остановить один/два инстанса и убедиться, что сервис доступен:

```bash
docker compose stop app1
for i in {1..6}; do curl -s -H 'Connection: close' http://localhost/api/whoami; echo; done
curl -s http://localhost/api/files | wc -c

docker compose stop app3
for i in {1..6}; do curl -s -H 'Connection: close' http://localhost/api/whoami; echo; done
curl -s http://localhost/api/files | wc -c

docker compose start app1 app3
```
