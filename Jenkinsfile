pipeline {
    agent any

    tools {
        jdk 'JDK 21'
        maven 'Maven 3.9'
    }

    environment {
        PUBLICACAO = 'C:\\Jenkins\\publicacao\\vinheria'
    }

    stages {
        stage('Build') {
            steps {
                echo 'Compilando os microsserviços da Vinheria Agnello'

                dir('produtos-service') {
                    bat 'mvn -B clean package'
                }

                dir('carrinho-service') {
                    bat 'mvn -B clean package'
                }
            }
        }

       stage('Preparar publicação') {
    steps {
        powershell '''
            $ErrorActionPreference = 'Stop'

            $destino = $env:PUBLICACAO

            New-Item -ItemType Directory -Force -Path $destino | Out-Null

            # Limpa a publicação anterior dentro desta pasta
            Get-ChildItem -LiteralPath $destino -Force |
                Remove-Item -Recurse -Force

            $produtos = Join-Path $destino 'produtos'
            $carrinho = Join-Path $destino 'carrinho'

            New-Item -ItemType Directory -Force -Path $produtos, $carrinho |
                Out-Null

            Copy-Item 'produtos-service/target/produtos-service.war' $produtos
            Copy-Item 'carrinho-service/target/carrinho-service.war' $carrinho
            Copy-Item 'docker-compose.yml' $destino
            Copy-Item 'produtos-service/Dockerfile' $produtos
            Copy-Item 'carrinho-service/Dockerfile' $carrinho
        '''
    }
}

        stage('Deploy simulado') {
            steps {
                echo 'Deploy da Vinheria Agnello'
                 powershell '''
                    Get-ChildItem -LiteralPath $env:PUBLICACAO -Recurse -File |
                        Select-Object -ExpandProperty FullName
                '''
            }
        }
    }

    post {
        success {
            echo 'Build e publicação simulada concluídos.'
        }
        failure {
            echo 'A pipeline falhou. Consulte os logs das etapas.'
        }
    }
}
