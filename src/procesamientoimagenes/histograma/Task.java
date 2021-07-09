package procesamientoimagenes.histograma;

import procesamientoimagenes.imagenes.Image;

class Task {
    public Integer beginningIndex;
    public Integer endingIndex;
    public Image image;

    public Task(Integer beginningIndex, Integer endingIndex, Image image){
        this.beginningIndex = beginningIndex;
        this.endingIndex = endingIndex;
        this.image = image;
    }
}
