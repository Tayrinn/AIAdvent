#!/bin/bash

echo "🚀 Управление сервисом мониторинга"
echo "=================================="

# Функция проверки статуса
check_status() {
    if pgrep -f "monitor_service.sh" > /dev/null; then
        echo "✅ Монитор запущен"
        echo "   PID: $(pgrep -f "monitor_service.sh")"
        return 0
    else
        echo "❌ Монитор не запущен"
        return 1
    fi
}

# Обработка команд
case "$1" in
    start)
        echo "🔧 Запуск монитора..."
        nohup ./monitor_service.sh > monitor.log 2>&1 &
        sleep 2
        check_status
        ;;
    stop)
        echo "🛑 Остановка монитора..."
        pkill -f monitor_service.sh
        sleep 1
        check_status
        ;;
    restart)
        echo "🔄 Перезапуск монитора..."
        pkill -f monitor_service.sh
        sleep 1
        nohup ./monitor_service.sh > monitor.log 2>&1 &
        sleep 2
        check_status
        ;;
    status)
        check_status
        ;;
    logs)
        if [ -f monitor.log ]; then
            tail -f monitor.log
        else
            echo "❌ Лог-файл не найден"
        fi
        ;;
    *)
        echo "📋 Использование:"
        echo "  $0 start   - запустить монитор"
        echo "  $0 stop    - остановить монитор"
        echo "  $0 restart - перезапустить монитор"
        echo "  $0 status  - проверить статус"
        echo "  $0 logs    - показать логи"
        echo ""
        check_status
        ;;
esac