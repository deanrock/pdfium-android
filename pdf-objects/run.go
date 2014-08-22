package main

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	"image"
	"image/color"
	//"image/draw"
	"image/png"
	"io/ioutil"
	//"log"
	"math/rand"
	"net/http"
	"os"
	"strconv"
	//"strings"
	//"github.com/BurntSushi/ty/fun"
	//"sort"
	"time"
)

type Document struct {
	Width  int
	Height int
	Rects  []Rect
}

type Rect struct {
	Left   int
	Top    int
	Width  int
	Height int
	Right int
	Bottom int
	Color  color.RGBA
}

type NewRect struct {
	Left   int
	Top    int
	Width  int
	Height int
	Right int
	Bottom int
	Color  color.RGBA
	Rects []Rect
}

func (nr *NewRect) AppendRect(r Rect) {
	nr.Rects = append(nr.Rects, r)

	if nr.Left > r.Left {
		nr.Left = r.Left
	}

	if nr.Top > r.Top {
		nr.Top = r.Top
	}

	if nr.Bottom < r.Bottom {
		nr.Bottom = r.Bottom
	}

	if nr.Right < r.Right {
		nr.Right = r.Right
	}
}

func drawGradient(m image.RGBA) {
	size := m.Bounds().Size()
	for x := 0; x < size.X; x++ {
		for y := 0; y < size.Y; y++ {
			color := color.RGBA{
				uint8(255 * x / size.X),
				uint8(255 * y / size.Y),
				55,
				255}
			m.Set(x, y, color)
		}
	}
}

func random(min, max int) int {
	return rand.Intn(max-min) + min
}

func drawRectangleNR(m image.RGBA, rect NewRect) {
	fmt.Printf("%d %d | %d %d\n", rect.Left, rect.Top, rect.Right, rect.Bottom)

	for x := rect.Left; x < rect.Right; x++ {
		m.Set(x, rect.Top, rect.Color)
		m.Set(x, rect.Bottom, rect.Color)
	}

	for y := rect.Top; y < rect.Bottom; y++ {
		m.Set(rect.Left, y, rect.Color)
		m.Set(rect.Right, y, rect.Color)
	}
}

func drawRectangle(m image.RGBA, rect Rect) {
	fmt.Printf("%d %d | %d %d\n", rect.Left, rect.Top, rect.Right, rect.Height)

	for x := rect.Left; x < rect.Right; x++ {
		m.Set(x, rect.Top, rect.Color)
		m.Set(x, rect.Bottom, rect.Color)
	}

	for y := rect.Top; y < rect.Bottom; y++ {
		m.Set(rect.Left, y, rect.Color)
		m.Set(rect.Right, y, rect.Color)
	}
}

func drawFullRectangle(m image.RGBA, xi int, yi int) {
	color := color.RGBA{
		uint8(0),
		uint8(0),
		uint8(0),
		255}

	limit := 10

	for x := xi - limit; x < xi+limit; x++ {
		for y := yi - limit; y < yi+limit; y++ {
			m.Set(x, y, color)
		}
	}
}

func getImage(w http.ResponseWriter, doc Document, click_x int, click_y int, name string) {
	width, height := doc.Width, doc.Height

	img, err := os.Open(name + ".png")
	defer img.Close()

	if err != nil {
		fmt.Printf("open error %v", err)
		return
	}

	raw, _, err := image.Decode(img)

	if err != nil {
		fmt.Printf("error %v", err)
		return
	}

	m := image.NewRGBA(image.Rect(0, 0, width, height))

	bounds := raw.Bounds()
	wi, hi := bounds.Max.X, bounds.Max.Y

	for x := 0; x < wi; x++ {
		for y := 0; y < hi; y++ {
			m.Set(x, y, raw.At(x, y))
		}
	}

	rand.Seed(time.Now().Unix())

	drawFullRectangle(*m, click_x, click_y)

	defaultColor := color.RGBA{255, 255, 255, 255}

	for i := 0; i < len(doc.Rects); i++ {
		doc.Rects[i].Color = defaultColor
	}

	for i, rect := range doc.Rects {
		doc.Rects[i].Bottom = rect.Top + rect.Height
		doc.Rects[i].Right = rect.Left + rect.Width
	}

	newRects := make(map[*Rect]NewRect)

	/*doc.Rects = fun.QuickSort(func(a, b Rect) bool {
		return a.Top < b.Top
	}, doc.Rects).([]Rect)*/

	

	for i := 0; i < len(doc.Rects); i++ {
		rect := doc.Rects[i]

		_, ok := newRects[&doc.Rects[i]]

		if ok {
			continue
		}

		newRect := NewRect{}
		
		newRect.Left = width
		newRect.Top = height
		newRect.Color = color.RGBA{
				uint8(random(0, 255)),
				uint8(random(0, 255)),
				uint8(random(0, 255)),
				255}

		c := doc.Rects[i].Color
		if c == defaultColor {

			//find nearby rectangles
			c = newRect.Color

			doc.Rects[i].Color = c
		}

		newRect.AppendRect(rect)

		margin := 10

		//rects := make([]int, 1)
		//rects = append(rects, i)

		//max := 0
		//start := doc.Width

		for i, rect2 := range doc.Rects {
			//diff := bottom - rect2.Top
			//fmt.Printf("diff %v\n", diff)
			/*if //diff < 15 && diff >= 0 &&
			//rect2.Top-margin < bottom &&
			//rect2.Top+margin > bottom &&
			rect2.Left < rect.Left+margin &&
				rect2.Left > rect.Left-margin {
				//rect2.Color == defaultColor {
				//c := color.RGBA{200, 200, 200, 255}
				doc.Rects[i].Color = c
				/*rects = append(rects, i)

				if rect2.Width > max {
					max = rect2.Width
				}

				if rect2.Left < start {
					start = rect2.Left
				}
			}*/
			if (rect2.Color == defaultColor) {
			if (((rect.Left > rect2.Left-margin && rect.Left < rect2.Left+margin) || (rect.Right > rect2.Right-margin && rect.Right < rect2.Right+margin)) && ((rect.Bottom > rect2.Top - margin && rect.Bottom < rect2.Top + margin) || (rect.Top > rect2.Bottom + margin && rect.Top < rect2.Bottom + margin))) {
				doc.Rects[i].Color = c

				newRect.AppendRect(rect2)

				}
			//}

			/*if (rect.Left > rect2.Left-margin && rect.Left < rect2.Left+margin)  &&
				(bottom > rect2.Top-margin && bottom < rect2.Top+margin) {
				//doc.Rects[i].Color = c
			}

			if (rect.Left > rect2.Left-margin && rect.Left < rect2.Left+margin) &&
				(bottom > rect2.Top - margin && bottom < rect2.Top + margin) {
				doc.Rects[i].Color = c
			}*/
		}
}
		/*for _, element := range rects {
			doc.Rects[element].Width = max
			doc.Rects[element].Left = start
		}*/

		newRects[&doc.Rects[i]] = newRect
	}

	for _, rect := range doc.Rects {
		/*if int(rect.Left) <= click_x &&
		int(doc.Height)-int(rect.Top) <= click_y &&
		int(rect.Left)+(int(rect.Right)-int(rect.Left)) > click_x &&
		(int(doc.Height)-int(rect.Top))+(int(rect.Top)-int(rect.Bottom)) > click_y {
		
		//fmt.Printf("%f %f -- %f %f     ", rect.Top, rect.Left, rect.Left, rect.Bottom)*/
		drawRectangle(*m,
			rect)

		//}
	}

	for _, nr := range newRects {
		//drawRectangleNR(*m, nr)
		fmt.Printf("%v\n", len(nr.Rects))
	}

	png.Encode(w, m)
}

func writeHeader(w http.ResponseWriter, code int, contentType string) {
	if code >= 0 {
		w.Header().Set("Content-Type", contentType)
		w.WriteHeader(code)
	}
}

func main() {
	name := os.Args[1] //strings.Replace(os.Args[1], ".pdf", "", -1)
	file, e := ioutil.ReadFile("./" + name + ".json")
	if e != nil {
		fmt.Printf("File error: %v\n", e)
		os.Exit(1)
	}

	var doc Document

	if err := json.Unmarshal(file, &doc); err != nil {
		fmt.Printf("%v\n", err)
	}

	//m := image.NewRGBA(image.Rect(0, 0, 640, 480))
	//blue := color.RGBA{0, 0, 255, 255}
	//draw.Draw(m, m.Bounds(), &image.Uniform{blue}, image.ZP, draw.Src)

	fmt.Printf("Results: %v\n", doc)

	r := gin.Default()
	r.GET("/ping", func(c *gin.Context) {
		c.String(200, "pong")
	})

	r.GET("/image/:x/:y", func(c *gin.Context) {
		writeHeader(c.Writer, 200, "image/png")
		x, _ := strconv.Atoi(c.Params.ByName("x"))
		y, _ := strconv.Atoi(c.Params.ByName("y"))

		getImage(c.Writer, doc, x, y, name)
	})

	// Listen and server on 0.0.0.0:8080
	r.Run(":8000")
}
