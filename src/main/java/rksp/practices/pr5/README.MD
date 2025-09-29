### Сборка и запуск кластера

```bash
cd ~/Desktop/java_projects/rksp_7sem
docker compose up --build -d
docker compose ps
```

Ожидаем 5 контейнеров: `app1..app4` и `proxy` (проброшен `80:80`).

---

### Проверка балансировки (least_conn)

Короткие соединения + подсчёт ответов по инстансам:

```bash
seq 1 100 | xargs -n1 -P16 -I{} curl -s -H 'Connection: close' -w '\n' http://localhost/api/whoami \
| grep -o 'app[0-9]\+' | sort | uniq -c
```

Ожидаемо распределение примерно ровное, например:

```
  25 app1
  25 app2
  28 app3
  22 app4
```

> Примечание: чтобы вывод было удобнее парсить, эндпоинт `/whoami` можно отдавать с переводом строки (`text/plain` + `instance + "\n"`), либо в `curl` использовать `-w '\n'`, как выше.

---

### Загрузка и список файлов (общий volume)

Создать 2 файла и загрузить:

```bash
echo "hello dds" > /tmp/file1.txt
dd if=/dev/urandom of=/tmp/random.bin bs=1M count=2
curl -s -F "files=@/tmp/file1.txt" -F "files=@/tmp/random.bin" http://localhost/api/upload
```

Посмотреть список:

```bash
curl -s http://localhost/api/files
```

Скачать первый из списка **без `jq`**:

```bash
NAME=$(curl -s http://localhost/api/files | sed -E 's/.*"name":"([^"]+)".*/\1/;q')
curl -OJ "http://localhost/upload-files/$NAME"
```

(или, если есть `jq`, скачать все)

```bash
for n in $(curl -s http://localhost/api/files | jq -r '.[].name'); do
  curl -s -OJ "http://localhost/upload-files/$n"
done
```

Проверить, что файлы видны на разных инстансах (общий том `uploads`):

```bash
docker compose exec app1 ls -l /upload-files
docker compose exec app3 ls -l /upload-files
```

---

### Отказоустойчивость

Остановить часть инстансов — сервис остаётся доступен:

```bash
docker compose stop app1
seq 1 12 | xargs -n1 -P6 -I{} curl -s -H 'Connection: close' -w '\n' http://localhost/api/whoami
curl -s http://localhost/api/files | wc -c

docker compose stop app3
seq 1 12 | xargs -n1 -P6 -I{} curl -s -H 'Connection: close' -w '\n' http://localhost/api/whoami
curl -s http://localhost/api/files | wc -c

docker compose start app1 app3
```

---

### Troubleshooting (коротко)

* **502 Bad Gateway**: проверь, что приложения подняты и слушают `:8080`

  ```bash
  docker compose ps
  docker compose logs --tail=100 proxy
  docker compose logs --tail=100 app1 app2 app3 app4
  ```
* **Nginx видит бэкенды**:

  ```bash
  docker compose exec proxy sh -lc 'curl -s http://app1:8080/api/whoami'
  ```
* **Пересборка**:

  ```bash
  docker compose down -v
  docker compose up --build -d
  ```