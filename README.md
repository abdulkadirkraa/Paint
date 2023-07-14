# Paint
Kullanıcının çizim yaptığı ve çizdiği çizimleri kaydedip gösteren android projesi

## Özellikler
* ROOM
* Coroutines
* View Binding
* RecyclerView
## Uygulama Videosu

https://github.com/abdulkadirkraa/Paint/assets/114829245/29a946f2-0ce7-4bb1-879d-38f9d95f9e46

***
Database'de paint objesini dosya olarak kaydedip veritabanında da dosya yolunu ve dosya ismini vererek kaydettim veritabanını fazla şişirmemek için
```kotlin
@Entity(tableName = "paints")
data class Paint(
    var imageName: String,
    var imagePath: String
) {
    @PrimaryKey(autoGenerate = true)
    var id=0

    @Ignore
    var isChecked=false
}
```
***

### Ayrıca Kullandığım Kütüphaneler
* #### DrawingCanvas
  Çizimleri yapmak için kullandığım kütüphanedir
  <br/>
  Çizim ekranı,kalem boyutu/kalınlığı,kalem renginin transparanlığı,geri alma,ileri alma,ekranı temizleme
  > https://github.com/Miihir79/DrawingCanvas-Library
* #### Color Picker Library for Android
  Çizim kaleminin rengini değiştirmek ve kullandığım renkleri tutmak için kullandığım kütüphanedir
  <br/>
  >https://github.com/Dhaval2404/ColorPicker
