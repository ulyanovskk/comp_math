import javafx.application.Platform
import jetbrains.datalore.base.registration.Disposable
import jetbrains.datalore.base.values.Color
import jetbrains.datalore.plot.MonolithicCommon
import jetbrains.datalore.vis.swing.jfx.DefaultPlotPanelJfx
import jetbrains.letsPlot.geom.geomPoint
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.intern.toSpec
import jetbrains.letsPlot.letsPlot
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.JFrame.EXIT_ON_CLOSE

object UIManager {
    private const val n = 2000
    private const val eps = 10.0 / n

    fun draw(rungeRes: List<Pair<Double, Double>>, milnRes: List<Pair<Double, Double>>, a: Double, b: Double, h: Double) {
        val plots = HashMap<String, Plot>()
        val n = ((b-a)/h).toInt()+1
        val wanted = mapOf<String, List<*>>(
            "xvar" to List(n) { j:Int-> rungeRes[j].first },
            "yvar" to List(n) { j:Int->
                rungeRes[j].second
            }
        )
        val wanted2 = mapOf<String, List<*>>(
            "xvar" to List(n) { j:Int-> milnRes[j].first },
            "yvar" to List(n) { j:Int->
                milnRes[j].second
            }
        )
        plots["Метод Рунге-Кутта"] = letsPlot(wanted) { x = "xvar"; y = "yvar" } + geomPoint(shape = 1, color = Color.RED, size = 5)
        plots["Метод Милна"] = letsPlot(wanted2) { x = "xvar"; y = "yvar" } + geomPoint(shape = 1, color = Color.DARK_GREEN, size = 5)


        val selectedPlotKey = plots.keys.first()
        val controller = Controller(
            plots,
            selectedPlotKey,
            false
        )

        val window = JFrame("Lets plot Kotlin")
        window.defaultCloseOperation = EXIT_ON_CLOSE
        window.contentPane.layout = BoxLayout(window.contentPane, BoxLayout.Y_AXIS)

        // Add controls
        val controlsPanel = Box.createHorizontalBox().apply {
            // Plot selector
            val plotButtonGroup = ButtonGroup()
            for (key in plots.keys) {
                plotButtonGroup.add(
                    JRadioButton(key, key == selectedPlotKey).apply {
                        addActionListener {
                            controller.plotKey = this.text
                        }
                    }
                )
            }

            this.add(Box.createHorizontalBox().apply {
                border = BorderFactory.createTitledBorder("Plot")
                for (elem in plotButtonGroup.elements) {
                    add(elem)
                }
            })

            // Preserve aspect ratio selector
            val aspectRadioButtonGroup = ButtonGroup()
            aspectRadioButtonGroup.add(JRadioButton("Стандарт", false).apply {
                addActionListener {
                    controller.preserveAspectRadio = true
                }
            })
            aspectRadioButtonGroup.add(JRadioButton("Широко", true).apply {
                addActionListener {
                    controller.preserveAspectRadio = false
                }
            })

            this.add(Box.createHorizontalBox().apply {
                border = BorderFactory.createTitledBorder("Ширина")
                for (elem in aspectRadioButtonGroup.elements) {
                    add(elem)
                }
            })
        }
        window.contentPane.add(controlsPanel)

        // Add plot panel
        val plotContainerPanel = JPanel(GridLayout())
        window.contentPane.add(plotContainerPanel)

        controller.plotContainerPanel = plotContainerPanel
        controller.rebuildPlotComponent()

        SwingUtilities.invokeLater {
            window.pack()
            window.size = Dimension(850, 400)
            window.setLocationRelativeTo(null)
            window.isVisible = true
        }
    }
}

private class Controller(
    private val plots: Map<String, Plot>,
    initialPlotKey: String,
    initialPreserveAspectRadio: Boolean
) {
    var plotContainerPanel: JPanel? = null
    var plotKey: String = initialPlotKey
        set(value) {
            field = value
            rebuildPlotComponent()
        }
    var preserveAspectRadio: Boolean = initialPreserveAspectRadio
        set(value) {
            field = value
            rebuildPlotComponent()
        }

    fun rebuildPlotComponent() {
        plotContainerPanel?.let {
            val container = plotContainerPanel!!
            // cleanup
            for (component in container.components) {
                if (component is Disposable) {
                    component.dispose()
                }
            }
            container.removeAll()

            // build
            container.add(createPlotPanel())
            container.parent?.revalidate()
        }
    }

    fun createPlotPanel(): JPanel {
        // Make sure JavaFX event thread won't get killed after JFXPanel is destroyed.
        Platform.setImplicitExit(false)

        val rawSpec = plots[plotKey]!!.toSpec()
        val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)

        return DefaultPlotPanelJfx(
            processedSpec = processedSpec,
            preserveAspectRatio = preserveAspectRadio,
            preferredSizeFromPlot = false,
            repaintDelay = 10,
        ) { messages ->
            for (message in messages) {
                println("[Example App] $message")
            }
        }
    }
}