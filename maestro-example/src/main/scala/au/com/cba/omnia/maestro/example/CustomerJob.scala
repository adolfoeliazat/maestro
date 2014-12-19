//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package au.com.cba.omnia.maestro.example

import org.apache.hadoop.hive.conf.HiveConf.ConfVars._

import com.twitter.scalding.{Config, Execution}

import au.com.cba.omnia.ebenezer.scrooge.hive.Hive

import au.com.cba.omnia.maestro.api._, Maestro._

import au.com.cba.omnia.maestro.example.thrift.Customer

/** Configuration for a customer execution example */
case class CustomerJobConfig(config: Config) {
  val maestro   = MaestroConfig(
    conf        = config,
    source      = "customer",
    domain      = "customer",
    tablename   = "customer"
  )
  val upload    = maestro.upload()
  val load      = maestro.load[Customer](none = "null")
  val dateTable = maestro.partitionedHiveTable[Customer, (String, String, String)](
    partition   = Partition.byDate(Fields[Customer].EffectiveDate),
    tablename   = "by_date"
  )
}

/** Customer file load job with an execution for the main program */
object CustomerJob extends MaestroJob {
  def job: Execution[JobStatus] = for {
    conf             <- Execution.getConfig.map(CustomerJobConfig(_))
    uploadInfo       <- upload(conf.upload)
    sources          <- uploadInfo.withSources
    (pipe, loadInfo) <- load[Customer](conf.load, uploadInfo.files)
    loadSuccess      <- loadInfo.withSuccess
    count            <- viewHive(conf.dateTable, pipe)
    if count == loadSuccess.actual
  } yield JobFinished

  def attemptsExceeded = Execution.from(JobNeverReady)   // Elided in the README
}
