import Overlay from "../../component/Overlay";

export default class TextOverlay extends Overlay {
    private text: egret.TextField;

    constructor() {
        super(true);

        this.visible = false;
        this.setSize(510, 50);

        let text = new egret.TextField();
        text.size = 24;
        text.width = 510;
        text.height = 50;
        text.verticalAlign = egret.VerticalAlign.MIDDLE;
        text.textAlign = egret.HorizontalAlign.CENTER;
        this.addChild(text);
        this.text = text;
    }

    show(text: string, duration: number = 0) {
        this.visible = false;
        this.parent.setChildIndex(this, 10000);
        this.text.text = text;
        this.visible = true;

        if (duration != 0) {
            setTimeout(() => {
                this.visible = false;
            }, duration);
        }
    }
}