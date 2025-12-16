type MessageHandler = (data: any) => void;
type EventHandler = (...args: any[]) => void;

export class faceWebSocketServer {
    private WS_URL: string = "ws://localhost:8080/ws";
    private ws: WebSocket | null = null;
    private messageHandler: MessageHandler | null = null;
    // 事件订阅表
    private events = new Map<string, Set<EventHandler>>();

    /**
     * 打开连接
     */
    public connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            this.ws = new WebSocket(this.WS_URL);

            this.ws.onopen = () => {
                console.log('websocket已连接');
                resolve();
            };

            this.ws.onerror = (err) => {
                console.error('WebSocket连接出错', err);
                reject(err);
            };

            this.ws.onmessage = (event) => {
                if (this.messageHandler) {
                    this.messageHandler(event.data);
                }
                this.emit("message", event.data);
            };
        });
    }
    /**
     * 直接发送消息（原来的方法，不等待确认）
     */
    public send(data: ArrayBuffer | ArrayBufferView | string): void {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;

        if (data instanceof ArrayBuffer) this.ws.send(data);
        else if (ArrayBuffer.isView(data)) this.ws.send(data.buffer);
        else if (typeof data === "string") this.ws.send(data);
    }

    // 发送后等待确认
    public sendWithAck(data: ArrayBuffer | ArrayBufferView | string): Promise<void> {
        return new Promise((resolve, reject) => {
            if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return reject("WebSocket未连接");

            const handleAck = (event: any) => {
                if (event === "ack") {
                    console.log('客户端收到回复消息');
                    this.off("message", handleAck);
                    this.emit("ack");
                    resolve();
                }else if(event === 'error'){
                    this.off("message",handleAck);
                    this.emit("error");
                }
            };

            this.on("message", handleAck);

            if (data instanceof ArrayBuffer) this.ws.send(data);
            else if (ArrayBuffer.isView(data)) this.ws.send(data.buffer);
            else if (typeof data === "string") this.ws.send(data);
        });
    }

    /**
     * 设置全局消息回调
     */
    public onMessage(handler: MessageHandler) {
        this.messageHandler = handler;
    }
    /** 事件订阅 */
    public on(event: string, handler: EventHandler) {
        if (!this.events.has(event)) this.events.set(event, new Set());
        this.events.get(event)!.add(handler);
    }

    /** 取消订阅 */
    public off(event: string, handler: EventHandler) {
        this.events.get(event)?.delete(handler);
    }

    /** 发布事件 */
    private emit(event: string, ...args: any[]) {
        this.events.get(event)?.forEach(fn => fn(...args));
    }

    /**
     * 关闭 WebSocket
     */
    public close(): void {
        this.ws?.close();
    }
}

let instance: faceWebSocketServer | null = null;
export const getFaceWebSocketServer = (): faceWebSocketServer => {
    if (!instance) {
        instance = new faceWebSocketServer();
    }
    return instance;
};
