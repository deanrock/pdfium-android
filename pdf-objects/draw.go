package main

import (
	"encoding/json"
	"fmt"
	"github.com/deanrock/go/img/pnm"
	"github.com/gin-gonic/gin"
	"image"
	"image/color"
	//"image/draw"
	"image/png"
	"strings"
	"io/ioutil"
	//"log"
	"math/rand"
	"net/http"
	"os"
	"os/exec"
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
	Right  int
	Bottom int
	Color  color.RGBA
}

type NewRect struct {
	Left   int
	Top    int
	Width  int
	Height int
	Right  int
	Bottom int
	Color  color.RGBA
	Rects  []Rect
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

func getImage(w http.ResponseWriter, name string) {
	out, err := exec.Command("./cpptest/pdfium_test", "pdf/"+name).Output()
	fmt.Printf("%s", out)

	file, e := ioutil.ReadFile("./pdf/" + name + ".0.json")
	if e != nil {
		fmt.Printf("File error: %v\n", e)
	}

	var doc Document

	if err := json.Unmarshal(file, &doc); err != nil {
		fmt.Printf("%v\n", err)
	}

	img, err := os.Open("./pdf/" + name + ".0.ppm")
	defer img.Close()

	if err != nil {
		fmt.Printf("open error %v", err)
		return
	}

	m, err := pnm.DecodeRGBA(img)

	if err != nil {
		fmt.Printf("error %v", err)
		return
	}

	rand.Seed(time.Now().Unix())

	for i := 0; i < len(doc.Rects); i++ {
		rect := doc.Rects[i]

		c := color.RGBA{
			uint8(random(0, 255)),
			uint8(random(0, 255)),
			uint8(random(0, 255)),
			255}

		rect.Color = c

		drawRectangle(*m,
			rect)

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
	r := gin.Default()
	r.LoadHTMLTemplates("templates/*")

	r.GET("/ping", func(c *gin.Context) {
		c.String(200, "pong")
	})

	r.GET("/pdf/:name", func(c *gin.Context) {
		name := c.Params.ByName("name")

		fileNames := make([]string, 0)

		files, _ := ioutil.ReadDir("./pdf")
		for _, f := range files {
			if strings.HasSuffix(f.Name(), ".pdf") {
				fileNames = append(fileNames, f.Name())
			}
		}

		obj := gin.H{"title": "Main website", "name": name, "files": fileNames}
		c.HTML(200, "pdf.tmpl", obj)
	})

	r.GET("/image/:name", func(c *gin.Context) {
		writeHeader(c.Writer, 200, "image/png")
		name := c.Params.ByName("name")

		getImage(c.Writer, name)
	})

	// Listen and server on 0.0.0.0:8080
	r.Run(":8000")
}
