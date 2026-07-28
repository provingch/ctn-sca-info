CREATE DATABASE  IF NOT EXISTS `mydbproyect` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;
USE `mydbproyect`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: mydbproyect
-- ------------------------------------------------------
-- Server version	5.5.5-10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alumno`
--

DROP TABLE IF EXISTS `alumno`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alumno` (
  `Curso` int(11) NOT NULL,
  `Especialidad` varchar(45) NOT NULL,
  `Nombre` varchar(45) NOT NULL,
  `idAlumno` int(11) NOT NULL,
  `Seccion` int(11) NOT NULL,
  PRIMARY KEY (`idAlumno`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alumno`
--

LOCK TABLES `alumno` WRITE;
/*!40000 ALTER TABLE `alumno` DISABLE KEYS */;
INSERT INTO `alumno` VALUES (3,'Electromecanica','Sosa Sofia',1,1),(3,'Electromecanica','Cabreara, Tatiana',2,1),(3,'Electromecanica','Villagra, Andres',3,1),(3,'Electromecanica','Benitez, francisco',4,1),(3,'Electromecanica','Martinez, Araceli',5,1),(3,'Electromecanica','Cohene, Cesar',6,1),(2,'Quimica Industrial','Viveros, Brailan',7,1),(2,'Quimica Industrial','Rodas, Nery',8,1),(2,'Quimica Industrial','Mestral, Cecilia',9,1),(1,'Electrónica','Troche, Alejandro',12,3),(1,'Electrónica','Maestral, Cecilia',13,3),(1,'Electrónica','Rodriguez, Alejandro',14,3);
/*!40000 ALTER TABLE `alumno` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `aspectos`
--

DROP TABLE IF EXISTS `aspectos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aspectos` (
  `id` int(11) NOT NULL,
  `Total` int(11) NOT NULL,
  `Logrado` varchar(45) NOT NULL,
  `Descripcion` varchar(70) NOT NULL,
  `Autoevaluacion_idAutoevaluacion` int(11) NOT NULL,
  PRIMARY KEY (`Autoevaluacion_idAutoevaluacion`),
  CONSTRAINT `fk_Aspectos_Autoevaluacion1` FOREIGN KEY (`Autoevaluacion_idAutoevaluacion`) REFERENCES `autoevaluacion` (`idAutoevaluacion`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `aspectos`
--

LOCK TABLES `aspectos` WRITE;
/*!40000 ALTER TABLE `aspectos` DISABLE KEYS */;
/*!40000 ALTER TABLE `aspectos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `autoevaluacion`
--

DROP TABLE IF EXISTS `autoevaluacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `autoevaluacion` (
  `idAutoevaluacion` int(11) NOT NULL,
  `Archivo` varchar(200) NOT NULL,
  `Fecha` date NOT NULL,
  `Profesor_idProfesor` int(11) NOT NULL,
  PRIMARY KEY (`idAutoevaluacion`,`Profesor_idProfesor`),
  KEY `fk_Autoevaluacion_Profesor1_idx` (`Profesor_idProfesor`),
  CONSTRAINT `fk_Autoevaluacion_Profesor1` FOREIGN KEY (`Profesor_idProfesor`) REFERENCES `profesor` (`idProfesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `autoevaluacion`
--

LOCK TABLES `autoevaluacion` WRITE;
/*!40000 ALTER TABLE `autoevaluacion` DISABLE KEYS */;
INSERT INTO `autoevaluacion` VALUES (1,'Documentos compartidos/Matias/Autoevaluacion/1-15-09-2025-675316_Evaluacion.pdf','2025-09-15',5),(2,'Documentos compartidos/Acevedo Torres, Kevin/Autoevaluacion/1-15-09-2025-814797_Evaluacion.pdf','2025-09-15',1);
/*!40000 ALTER TABLE `autoevaluacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cronogramaanual`
--

DROP TABLE IF EXISTS `cronogramaanual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cronogramaanual` (
  `Curso` int(11) NOT NULL,
  `Seccion` int(11) NOT NULL,
  `Especialidad` varchar(45) NOT NULL,
  `idCronograma` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `Profesor_idProfesor` int(11) NOT NULL,
  `Archivo` varchar(200) NOT NULL,
  `Materia` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`idCronograma`,`Profesor_idProfesor`),
  KEY `fk_CronogramaAnual_Profesor1_idx` (`Profesor_idProfesor`),
  CONSTRAINT `fk_CronogramaAnual_Profesor1` FOREIGN KEY (`Profesor_idProfesor`) REFERENCES `profesor` (`idProfesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cronogramaanual`
--

LOCK TABLES `cronogramaanual` WRITE;
/*!40000 ALTER TABLE `cronogramaanual` DISABLE KEYS */;
INSERT INTO `cronogramaanual` VALUES (3,1,'Electromecanica',1,'2025-09-15',1,'Documentos compartidos/Acevedo Torres, Kevin/CronogramaAnual/1-15-09-2025-101982_ANUAL.xlsx','Informatica Especifica'),(1,3,'Electrónica',2,'2025-09-15',2,'Documentos compartidos/Jhon Anderson/CronogramaAnual/1-15-09-2025-716725_ANUAL.xlsx','Ciencias');
/*!40000 ALTER TABLE `cronogramaanual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `evaluacion`
--

DROP TABLE IF EXISTS `evaluacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluacion` (
  `idEvaluacion` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `Archivo` varchar(200) NOT NULL,
  `Profesor_idProfesor` int(11) NOT NULL,
  PRIMARY KEY (`idEvaluacion`,`Profesor_idProfesor`),
  KEY `fk_Evaluacion_Profesor1_idx` (`Profesor_idProfesor`),
  CONSTRAINT `fk_Evaluacion_Profesor1` FOREIGN KEY (`Profesor_idProfesor`) REFERENCES `profesor` (`idProfesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evaluacion`
--

LOCK TABLES `evaluacion` WRITE;
/*!40000 ALTER TABLE `evaluacion` DISABLE KEYS */;
INSERT INTO `evaluacion` VALUES (1,'2025-09-15','Documentos compartidos/Matias/Evaluacion/1-15-09-2025-124248_Evaluacion.pdf',5),(2,'2025-09-15','Documentos compartidos/Blas Emanuel/Evaluacion/1-15-09-2025-949800_Evaluacion.pdf',4);
/*!40000 ALTER TABLE `evaluacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hoja_catedra`
--

DROP TABLE IF EXISTS `hoja_catedra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hoja_catedra` (
  `IdCatedra` int(11) NOT NULL,
  `Especialidad` varchar(45) NOT NULL,
  `Curso` int(11) NOT NULL,
  `Seccion` int(11) NOT NULL,
  `Materia` varchar(45) DEFAULT NULL,
  `Fecha` date NOT NULL,
  `Descripcion` varchar(45) NOT NULL,
  `Profesor_idProfesor` int(11) NOT NULL,
  PRIMARY KEY (`IdCatedra`,`Profesor_idProfesor`),
  KEY `fk_Hoja_Catedra_Profesor1_idx` (`Profesor_idProfesor`),
  CONSTRAINT `fk_Hoja_Catedra_Profesor1` FOREIGN KEY (`Profesor_idProfesor`) REFERENCES `profesor` (`idProfesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hoja_catedra`
--

LOCK TABLES `hoja_catedra` WRITE;
/*!40000 ALTER TABLE `hoja_catedra` DISABLE KEYS */;
/*!40000 ALTER TABLE `hoja_catedra` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `informe`
--

DROP TABLE IF EXISTS `informe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `informe` (
  `id` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `Archivo` varchar(200) NOT NULL,
  `Profesor_idProfesor` int(11) NOT NULL,
  KEY `fk_Informe_Profesor1_idx` (`Profesor_idProfesor`),
  CONSTRAINT `fk_Informe_Profesor1` FOREIGN KEY (`Profesor_idProfesor`) REFERENCES `profesor` (`idProfesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `informe`
--

LOCK TABLES `informe` WRITE;
/*!40000 ALTER TABLE `informe` DISABLE KEYS */;
INSERT INTO `informe` VALUES (1,'2025-09-15','Documentos compartidos/Jhon Anderson/Informe/1-15-09-2025-048518_informe.pdf',2),(2,'2025-09-15','Documentos compartidos/Matias/Informe/1-15-09-2025-663012_informe.pdf',5);
/*!40000 ALTER TABLE `informe` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `memo`
--

DROP TABLE IF EXISTS `memo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `memo` (
  `id` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `Archivo` varchar(200) NOT NULL,
  `Profesor_idProfesor` int(11) NOT NULL,
  KEY `fk_Memo_Profesor1_idx` (`Profesor_idProfesor`),
  CONSTRAINT `fk_Memo_Profesor1` FOREIGN KEY (`Profesor_idProfesor`) REFERENCES `profesor` (`idProfesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `memo`
--

LOCK TABLES `memo` WRITE;
/*!40000 ALTER TABLE `memo` DISABLE KEYS */;
INSERT INTO `memo` VALUES (1,'2025-09-15','Documentos compartidos/Matias/Memo/1-15-09-2025-288670_MEMO.pdf',5),(2,'2025-09-15','Documentos compartidos/Acevedo Torres, Kevin/Memo/1-15-09-2025-770639_MEMO.pdf',1),(3,'2025-09-15','Documentos compartidos/Marcos Daniel/Memo/1-15-09-2025-599614_MEMO.pdf',3);
/*!40000 ALTER TABLE `memo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `plan_estudio`
--

DROP TABLE IF EXISTS `plan_estudio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plan_estudio` (
  `Especialidad` int(11) NOT NULL,
  `Temas_Planificacion_Diaria_idPlani` int(11) NOT NULL,
  PRIMARY KEY (`Temas_Planificacion_Diaria_idPlani`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plan_estudio`
--

LOCK TABLES `plan_estudio` WRITE;
/*!40000 ALTER TABLE `plan_estudio` DISABLE KEYS */;
/*!40000 ALTER TABLE `plan_estudio` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `planificacion_diaria`
--

DROP TABLE IF EXISTS `planificacion_diaria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `planificacion_diaria` (
  `Curso` int(11) NOT NULL,
  `Seccion` int(11) NOT NULL,
  `idPlani` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `Archivo` varchar(200) NOT NULL,
  `Especialidad` varchar(45) NOT NULL,
  `Profesor_idProfesor` int(11) NOT NULL,
  `Materia` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`idPlani`,`Profesor_idProfesor`),
  KEY `fk_Planificacion_Diaria_Profesor1_idx` (`Profesor_idProfesor`),
  CONSTRAINT `fk_Planificacion_Diaria_Profesor1` FOREIGN KEY (`Profesor_idProfesor`) REFERENCES `profesor` (`idProfesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `planificacion_diaria`
--

LOCK TABLES `planificacion_diaria` WRITE;
/*!40000 ALTER TABLE `planificacion_diaria` DISABLE KEYS */;
INSERT INTO `planificacion_diaria` VALUES (1,3,1,'2025-09-15','Documentos compartidos/Blas Emanuel/Planificacion_Diaria/1-15-09-2025-907083_CURRICULAR.xlsx','Electrónica',4,' Matematica'),(2,1,2,'2025-09-15','Documentos compartidos/Blas Emanuel/Planificacion_Diaria/1-15-09-2025-347951_CURRICULAR.xlsx','Quimica Industrial',4,' Matematica'),(3,1,3,'2025-09-15','Documentos compartidos/Marcos Daniel/Planificacion_Diaria/1-15-09-2025-651988_CURRICULAR.xlsx','Electromecanica',3,' Literatura'),(2,2,4,'2025-09-15','Documentos compartidos/Jhon Anderson/Planificacion_Diaria/1-15-09-2025-683504_CURRICULAR.xlsx','Quimica Industrial',2,'Ciencias');
/*!40000 ALTER TABLE `planificacion_diaria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `presencia`
--

DROP TABLE IF EXISTS `presencia`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `presencia` (
  `Id_alumno` int(11) NOT NULL,
  `id_profe` int(11) DEFAULT NULL,
  `Presencia` varchar(45) DEFAULT NULL,
  KEY `fk_Presencia_1_idx` (`Id_alumno`),
  KEY `fk_Presencia_2_idx` (`id_profe`),
  CONSTRAINT `fk_Presencia_1` FOREIGN KEY (`Id_alumno`) REFERENCES `alumno` (`idAlumno`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Presencia_2` FOREIGN KEY (`id_profe`) REFERENCES `hoja_catedra` (`IdCatedra`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `presencia`
--

LOCK TABLES `presencia` WRITE;
/*!40000 ALTER TABLE `presencia` DISABLE KEYS */;
/*!40000 ALTER TABLE `presencia` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `profesor`
--

DROP TABLE IF EXISTS `profesor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profesor` (
  `idProfesor` int(11) NOT NULL,
  `Nombre` varchar(45) NOT NULL,
  `CI` int(11) NOT NULL,
  `Usuario_idUsuario` int(11) NOT NULL,
  PRIMARY KEY (`idProfesor`,`Nombre`,`CI`,`Usuario_idUsuario`),
  KEY `fk_Profesor_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Profesor_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `profesor`
--

LOCK TABLES `profesor` WRITE;
/*!40000 ALTER TABLE `profesor` DISABLE KEYS */;
INSERT INTO `profesor` VALUES (1,'Acevedo Torres, Kevin',5804696,7),(2,'Jhon Anderson',6336600,8),(3,'Marcos Daniel',6291606,9),(4,'Blas Emanuel',7370725,10),(5,'Matias',7221574,11);
/*!40000 ALTER TABLE `profesor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `temas_cronogramaanual`
--

DROP TABLE IF EXISTS `temas_cronogramaanual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temas_cronogramaanual` (
  `id` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `MetodoEvaluacion` varchar(45) NOT NULL,
  `Descripcion` varchar(45) NOT NULL,
  `CronogramaAnual_idCronograma` int(11) NOT NULL,
  KEY `Foraneo_hecho` (`CronogramaAnual_idCronograma`),
  CONSTRAINT `Foraneo_hecho` FOREIGN KEY (`CronogramaAnual_idCronograma`) REFERENCES `cronogramaanual` (`idCronograma`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `temas_cronogramaanual`
--

LOCK TABLES `temas_cronogramaanual` WRITE;
/*!40000 ALTER TABLE `temas_cronogramaanual` DISABLE KEYS */;
/*!40000 ALTER TABLE `temas_cronogramaanual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `temas_planificacion_diaria`
--

DROP TABLE IF EXISTS `temas_planificacion_diaria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temas_planificacion_diaria` (
  `id` int(11) NOT NULL,
  `Fecha` date NOT NULL,
  `MetodoEvaluacion` varchar(45) NOT NULL,
  `Descripcion` varchar(45) NOT NULL,
  `Planificacion_Diaria_idPlani` int(11) NOT NULL,
  KEY `fk_Temas_Palanificacion_Diaria_1_idx` (`Planificacion_Diaria_idPlani`),
  CONSTRAINT `fk_Temas_Palanificacion_Diaria_1` FOREIGN KEY (`Planificacion_Diaria_idPlani`) REFERENCES `planificacion_diaria` (`idPlani`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `temas_planificacion_diaria`
--

LOCK TABLES `temas_planificacion_diaria` WRITE;
/*!40000 ALTER TABLE `temas_planificacion_diaria` DISABLE KEYS */;
/*!40000 ALTER TABLE `temas_planificacion_diaria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `idUsuario` int(11) NOT NULL,
  `Contrasena` varchar(45) NOT NULL,
  `Nombre` varchar(45) NOT NULL,
  PRIMARY KEY (`idUsuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (7,'123','coordinador'),(8,'locura','Kevin'),(9,'locura','Jhon'),(10,'locura','Marcos'),(11,'locura','Blas'),(12,'locura','Matias');
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-08 22:12:08
